/*
 * Copyright 2019 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.core.flow.service;

import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.YmlDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.event.FlowConfirmedEvent;
import com.flowci.core.flow.event.FlowDeletedEvent;
import com.flowci.core.plugin.service.PluginService;
import com.flowci.domain.Vars;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.NotFoundException;
import com.flowci.tree.Node;
import com.flowci.tree.YmlParser;
import com.flowci.util.StringHelper;
import com.google.common.base.Strings;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service
public class YmlServiceImpl implements YmlService {

    @Autowired
    private Template defaultYmlTemplate;

    @Autowired
    private YmlDao ymlDao;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private CronService cronService;

    @Autowired
    private PluginService pluginService;

    //====================================================================
    //        %% Public function
    //====================================================================

    @Override
    public List<Node> ListChildren(Flow flow) {
        Optional<Yml> optional = ymlDao.findById(flow.getId());
        if (!optional.isPresent()) {
            return Collections.emptyList();
        }

        Node root = YmlParser.load(flow.getName(), optional.get().getRaw());
        return root.getChildren();
    }

    @Override
    public Yml getYml(Flow flow) {
        Optional<Yml> optional = ymlDao.findById(flow.getId());
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("No yml defined for flow {0}", flow.getName());
    }

    @Override
    public Yml saveYml(Flow flow, String yml) {
        if (Strings.isNullOrEmpty(yml)) {
            throw new ArgumentException("Yml content cannot be null or empty");
        }

        Node root = YmlParser.load(flow.getName(), yml);

        // verify plugin and throw NotFoundException if not exist
        for (Node child : root.getChildren()) {
            if (child.hasPlugin()) {
                pluginService.get(child.getPlugin());
            }
        }

        Yml ymlObj = new Yml(flow.getId(), yml);
        ymlObj.setCreatedBy(sessionManager.getUserId());
        ymlDao.save(ymlObj);

        // sync flow envs from yml root envs
        Vars<String> vars = flow.getVariables();
        vars.clear();
        vars.merge(root.getEnvironments());
        flowDao.save(flow);

        // update cron task
        cronService.update(flow, root, ymlObj);
        return ymlObj;
    }

    //====================================================================
    //        %% Internal events
    //====================================================================

    @EventListener
    public void deleteYmlOnFlowDeleted(FlowDeletedEvent event) {
        try {
            Yml yml = getYml(event.getFlow());
            ymlDao.delete(yml);
        } catch (NotFoundException ignore) {

        }
    }

    @EventListener
    public void createYmlFromTemplate(FlowConfirmedEvent event) {
        saveYml(event.getFlow(), getTemplateYml());
    }

    private String getTemplateYml() {
        VelocityContext context = new VelocityContext();

        try (StringWriter sw = new StringWriter()) {
            defaultYmlTemplate.merge(context, sw);
            return sw.toString();
        } catch (IOException e) {
            return StringHelper.EMPTY;
        }
    }
}
